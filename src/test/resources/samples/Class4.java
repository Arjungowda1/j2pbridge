class Class4{
    int bar = 4;

    static class Inner{
        public String var = "Inner class"
    }

    class NestedInner {
        public String y = "somenull";
    }

    public static void main(String[] args) {
        Class4 c4 = new Class4();

        Inner si = new Inner();
        System.out.println(si.var);

        NestedInner ni = new NestedInner();
        System.out.println(ni.y);
    }
}