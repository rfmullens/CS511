import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class Bakery implements Runnable {
    private static final int TOTAL_CUSTOMERS = 200;
    private static final int ALLOWED_CUSTOMERS = 50;
    private static final int FULL_BREAD = 20;
    private Map<BreadType, Integer> availableBread;
    private ExecutorService executor;
    private float sales = 0;

    // TODO
    //One semaphore for access to each shelf
    public static Semaphore accessRye = new Semaphore(1);
    public static Semaphore accessSourdough = new Semaphore(1);
    public static Semaphore accessWonder = new Semaphore(1);

    public static Semaphore accessCheckout = new Semaphore(1);

    // Semaphores
    //Use the bread map to keep track of bread
    final static Semaphore Cashiers = new Semaphore(4);
    
    /**
     * Remove a loaf from the available breads and restock if necessary
     */
    public void takeBread(BreadType bread) {
        int breadLeft = availableBread.get(bread);
        if (breadLeft > 0) {
            availableBread.put(bread, breadLeft - 1);
        } else {
            System.out.println("No " + bread.toString() + " bread left! Restocking...");
            // restock by preventing access to the bread stand for some time
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            availableBread.put(bread, FULL_BREAD - 1);
        }
    }

    /**
     * Add to the total sales
     */
    public void addSales(float value) {
        sales += value;
    }

    /**
     * Run all customers in a fixed thread pool
     */
    public void run() {
        availableBread = new ConcurrentHashMap<BreadType, Integer>();
        availableBread.put(BreadType.RYE, FULL_BREAD);
        availableBread.put(BreadType.SOURDOUGH, FULL_BREAD);
        availableBread.put(BreadType.WONDER, FULL_BREAD);

        // TODO (partially done)
        //Creates pool for the capacity of threads, Limits number of threads 
        ExecutorService pool = Executors.newFixedThreadPool(ALLOWED_CUSTOMERS); 

        //creates customers...
        //and then executes them
        for(int i=0; i<TOTAL_CUSTOMERS; i++){
            Customer newCustomer = new Customer(this);
            pool.execute(newCustomer); 
            //initialize customers from the total customer pool here
        }

        //pool's closed
        pool.shutdown();
    }
}