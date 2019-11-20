package main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.types.Row;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * The accumulator is used to keep a running sum and a count. The {@code getResult} method
 * computes the average.
 */
public class FeatureAggregator implements AggregateFunction<Tuple2<String, Double>, LinkedHashMap<LocalDate, Double>, ObjectNode> {
    private static DateTimeFormatter dateTimeFormatter;

    public FeatureAggregator(String datePattern) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(datePattern);
    }

    @Override
    public LinkedHashMap<LocalDate, Double> createAccumulator() {
        return new LinkedHashMap<>();
    }

    @Override
    public LinkedHashMap<LocalDate, Double> add(Tuple2<String, Double> value, LinkedHashMap<LocalDate, Double> accumulator) {
        accumulator.put(LocalDate.from(dateTimeFormatter.parse(value.f0)), value.f1);
        return accumulator;
    }

    @Override
    public ObjectNode getResult(LinkedHashMap<LocalDate, Double> accumulator) {
//        Set<LocalDate> keySet = accumulator.keySet();
//        ArrayList<LocalDate> dates = new ArrayList<>();
//        dates.addAll(keySet);
//        dates.sort(LocalDate::compareTo);
//        System.out.println("Expected: dates from early to late"); //debuggin TODO remove
//        String dateString = "";
//        for(int i=0; i<dates.size(); i++)
//            dateString += "\n"+dates.get(i).toString();
//        System.out.println(dateString);
        if(accumulator.size() < 17)
            return null;
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode data = objectMapper.createArrayNode();
        for(LocalDate key : accumulator.keySet()) {
            data.add(objectMapper.createObjectNode()
            .put("date", key.format(dateTimeFormatter))
            .put("sales", accumulator.get(key)));
        }
        ObjectNode result = objectMapper.createObjectNode().set("data", data);

        return result;
    }

    @Override
    public LinkedHashMap<LocalDate, Double> merge(LinkedHashMap<LocalDate, Double> a, LinkedHashMap<LocalDate, Double> b) {
        a.putAll(b);
        return a;
    }
}