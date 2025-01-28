class Class3 {
    public int method(int x) {
        System.out.println(x);
        return x;
    }

    public static void main(String[] args) {
        Class3 c = new Class3();
        System.out.println(c.method(3));
    }
}