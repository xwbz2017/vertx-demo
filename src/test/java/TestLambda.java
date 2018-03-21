
public class TestLambda {

    public static void main(String[] args) {
        HelloLambda aa = () -> System.out.println("hello! Lambda.");
        HelloLambda2 a =  name -> System.out.println("hello! Lambda. from " + name);
        HelloLambda3 aaa =  (name, times) -> {
        for (int i=0;i<times;i++)
            System.out.println("hello! Lambda. from " + name);
        };
        aaa.sayHello("狗子", 3);

        HelloLambda4 aaaa =  (name) -> {
            String result = "hello! Lambda. from " + name;
            System.out.println(result);
            return result;
        };
        aaa.sayHello("狗子", 3);

        HelloLambda b = TestLambda::helloLambda;
    }

    interface HelloLambda3{
        void sayHello(String name, int times);
    }

    interface HelloLambda4{
        String sayHello(String name);
    }


    interface HelloLambda2{
        void sayHello(String name);
    }



    interface HelloLambda{
        void sayHello();
    }

    public static void helloLambda(){
        System.out.println("hello! Lambda");
    }

}
