class IfStmt0{
    public static void main(String[] args) {
        int var = 0;

        if(true){
            System.out.println("true");
        } else{
            System.out.println("false");
        }

        if(var == 0)
            System.out.println("its 0");
        else if (var == 1)
            System.out.println("its 1");
        else
            System.out.println("I dont know");
    }
}