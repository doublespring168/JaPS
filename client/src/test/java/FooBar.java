/**
 * Created by spring on 27.03.2017.
 */
public class FooBar {

    private String foo;

    public FooBar(String foo) {

        this.foo = foo;
    }

    @Override
    public String toString() {

        return "FooBar{" +
                "foo='" + foo + '\'' +
                '}';
    }
}
