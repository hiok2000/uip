import ray
from consumer_producer import KafkaConsumer, KafkaProducer
from time import sleep

@ray.remote
def write_results(message_queue_actor, output_topic, bootstrap_server):
    """create producer and poll the MessageQueueActor periodically for new messages"""
    producer = KafkaProducer(output_topic, bootstrap_server)
 
    while True:
        while ray.get(message_queue_actor.hasNext.remote()):
            producer._write_to_topic(ray.get(message_queue_actor.next.remote()))
        sleep(1)

@ray.remote
def compute(message_queue_actor, model, input_topic, bootstrap_server):
    """poll periodically, make a prediction and write the result to the message queue Actor"""
    consumer = KafkaConsumer(input_topic, bootstrap_server)
    model_obj_id = ray.put(model)
    models = []
    model_index = 0
    for _ in range(2):
        model = ModelActor(message_queue_actor, model_obj_id)
        models.append(model)

    while True:
        msg = consumer.poll(1.0)
        if(msg == None):
            sleep(1)
        else:
            models[model_index].predict.remote(msg.value)
            model_index = (model_index + 1) % len(models)

@ray.remote
class MessageQueueActor():
    def __init__(self):
        self.messages = []

    def push(self, message):
        self.messages.append(message)

    def next(self):
        return self.messages.pop(0)

    def hasNext(self):
        return len(self.messages) > 0

@ray.remote
class ModelActor():
    def __init__(self, message_queue_actor, model_obj_id):
        self.model = ray.get(model_obj_id)
        self.message_queue = message_queue_actor

    def predict(self, features):
        prediction = self.model.predict(features, batch_size=1)
        self.message_queue.push.remote(prediction)

def run(model, input_topic, output_topic, bootstrap_server):
    ray.init()
    message_queue_actor = MessageQueueActor()
    write_results.remote(message_queue_actor, output_topic, bootstrap_server)
    compute.remote(message_queue_actor, model, input_topic, bootstrap_server)
