public class Class2 {
    public int var;
    public int a = 1;
    public long v1, v2 = 3000000;
    public int z = 3, b;

    public static void main(String[] args) {
        Class2 class2 = new Class2();
        System.out.println(class2.var);
        System.out.println(class2.a);
        class2.z++;
        System.out.println(class2.z);
    }
}