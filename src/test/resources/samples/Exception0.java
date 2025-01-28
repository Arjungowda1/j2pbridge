import java.lang.RuntimeException;

class Exception0{
    public static void main(String[] args) {
        try {
            throw new RuntimeException("Exception");
        }catch (Exception exception){
            System.out.println("Caught");
        }
    }
}