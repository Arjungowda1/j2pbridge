class BasicType2 {
    int i;
    float f;
    double d;
    char c;
    long l;
    byte b;
    short s;

    public static void main(String[] args){
        BasicType2 basicType2 = new BasicType2();
        String.out.println(basicType2.b);
        System.out.println(basicType2.s);
        System.out.println(basicType2.i == 0 ? "yess" : "noo");
    }
}