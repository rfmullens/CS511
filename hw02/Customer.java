import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Customer implements Runnable {
    private Bakery bakery;
    private Random rnd;
    private List<BreadType> shoppingCart;
    private int shopTime;
    private int checkoutTime;

    /**
     * Initialize a customer object and randomize its shopping cart
     */
    public Customer(Bakery bakery) {
        // TODO
        
        this.bakery = bakery;
        this.rnd = new Random();
        this.shoppingCart = new ArrayList<BreadType>();
        this.shopTime =  rnd.nextInt(4); //randomly creates a time that the customer will be shopping between 0 and 200 ms
        this.checkoutTime = rnd.nextInt(3); //randomly creates a time that the customer will be checking out between 0 and 100 ms
        fillShoppingCart(); //Fills the customers shopping cart with what they want to buy (more of a shopping list i think)
    }

    /**
     * Run tasks for the customer
     */
    public void run() {
        // TODO
        //This function models a customers journey through the store
        //Print out when a customer starts shopping, takes an item, buys and finishes 4 places
        
        System.out.println("A wild customer appears " + toString()); //prints out a representation of each customer
        
        //make sure ot get all bread properly and atomicly
        for(int i = 0; i < shoppingCart.size(); i++ ){
            try{
            if(shoppingCart.contains(BreadType.RYE)){ //shopping cart contains x bread
            Bakery.accessRye.acquire(); //Checks to see if the customer can get to the shelf
            Thread.sleep(shopTime); // sleeps for the shop time
            bakery.takeBread(BreadType.RYE); //takes the bread and updates
            System.out.println(toString() + " Reaches and takes some RYE bread");
            //print the bread
            Bakery.accessRye.release();
            }
            if(shoppingCart.contains(BreadType.SOURDOUGH)){
            Bakery.accessSourdough.acquire();
            bakery.takeBread(BreadType.SOURDOUGH);
            System.out.println(toString() + " Doughs not really know what to take but takes some SOURDOUGH bread");
            Thread.sleep(shopTime);
            Bakery.accessSourdough.release();
            }
            if(shoppingCart.contains(BreadType.WONDER)){
            Bakery.accessWonder.acquire();
            Thread.sleep(shopTime);
            bakery.takeBread(BreadType.WONDER);
            System.out.println(toString() + " Wonders what to take, and eventually settles on WONDER bread");
            Bakery.accessWonder.release();
            }
            }catch (InterruptedException exc) { 
                System.out.println("Failed to get something");
        }

        //go to cashiers when one is avaiable and update sales
        try{
        Bakery.Cashiers.acquire();
        Thread.sleep(checkoutTime);
        bakery.addSales(getItemsValue());
        System.out.println("check out for " + getItemsValue() + " " + toString());
        //insert totaling sales and other stuff
        Bakery.Cashiers.release();
        }
        catch (InterruptedException exc) { 
                System.out.println("Failed to get something");
        }

        
    }
    }

    /**
     * Return a string representation of the customer
     */
    public String toString() {
        return "Customer " + hashCode() + ": shoppingCart=" + Arrays.toString(shoppingCart.toArray()) + ", shopTime=" + shopTime + ", checkoutTime=" + checkoutTime;
    }

    /**
     * Add a bread item to the customer's shopping cart
     */
    private boolean addItem(BreadType bread) {
        // do not allow more than 3 items, chooseItems() does not call more than 3 times
        if (shoppingCart.size() >= 3) {
            return false;
        }
        shoppingCart.add(bread);
        return true;
    }

    /**
     * Fill the customer's shopping cart with 1 to 3 random breads
     */
    private void fillShoppingCart() {
        int itemCnt = 1 + rnd.nextInt(3);
        while (itemCnt > 0) {
            addItem(BreadType.values()[rnd.nextInt(BreadType.values().length)]);
            itemCnt--;
        }
    }

    /**
     * Calculate the total value of the items in the customer's shopping cart
     */
    private float getItemsValue() {
        float value = 0;
        for (BreadType bread : shoppingCart) {
            value += bread.getPrice();
        }
        return value;
    }
}