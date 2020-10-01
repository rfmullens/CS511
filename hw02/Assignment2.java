/* start the simulation */
public class Assignment2 {
    public static void main(String[] args) {
        System.out.println("A wild customer appears ");
        Thread thread = new Thread(new Bakery());
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
