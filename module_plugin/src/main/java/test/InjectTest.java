package test;
//visit(V1_6, ACC_PUBLIC | ACC_SUPER , "org/more/test/asm/simple/TestBean",
//      null, "java/lang/Object", null)
public class InjectTest {
        private String stringData;
//        stringData字段等价于：
//        visitField(ACC_PRIVATE, "stringData", "Ljava/lang/String;", null, null)
    public void test() {

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
