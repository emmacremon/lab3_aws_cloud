package emse;

import java.util.List;

public class Calculs {

    /* Calculate the Total Number of Sales */
    public static int countSales(List<List<String>> records){
        int numSales = 0;
        for (List<String> row:records){
            if (Float.parseFloat(row.get(3))> 0){
                numSales++;
            }
        }
        return numSales;
    }
    
    /* Calculate the Total Amount of Sold */
    public static float totalAmountSold(List<List<String>> records){
        float totalAmountSold = 0;
        for (List<String> row:records){
            float amountSold = Float.parseFloat(row.get(3));
            if (amountSold> 0){
                totalAmountSold+=amountSold;
            }
        }
        return totalAmountSold;
    }
    
    /* Calculate the Average Sold */
    public static float averageSold(List<List<String>> records){
        float averageSold = 0;
        int countSales = 0;
        for (List<String> row :records){
            float amountSold = Float.parseFloat(row.get(3));
            if (amountSold> 0){
                averageSold+=amountSold;
                countSales++;
            }
        }
        averageSold = averageSold/countSales;
        return averageSold;
    }

}
