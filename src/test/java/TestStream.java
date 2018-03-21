import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class TestStream {

    public static void main(String[] args) {
        String[] arr = {"123", "456", "789"};
        List<String> list = Arrays.asList(arr);
        for(String s : list){
            System.out.print(s);
        }

        new HashSet<>(list).forEach(System.out::print);

        list.parallelStream().forEach(System.out::print);
        list.stream().forEach(s -> System.out.print(s));
        list.forEach(s -> System.out.print(s));

        Stream.of(arr).forEach(System.out::print);

//        Collections.singleton()
        Collections.singletonMap("key", "poker").forEach((k, v) -> {
            System.out.println(k);
            System.out.println(v);
        });
    }
}
