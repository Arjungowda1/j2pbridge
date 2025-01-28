class Class6 {
    int b = 4;

    private void x() {
        System.out.println('x');
    }

    private void y() {
        System.out.println(b);
    }

    private void empty() {
    }

    private void z(int a) {
        System.out.println(a);
    }

    public static void main(String[] args) {
        System.out.println(b);

        Class6 c = new Class6();
        c.x();
        c.y();
        c.z(3);
    }
}