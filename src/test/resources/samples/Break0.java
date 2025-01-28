class Break0 {
    public static void main(String[] args) {
        int foo = 0;
        while (foo < 100) {
            if (foo == 3) {
                break;
            }
            foo++;
            System.out.println(foo);
        }
    }
}