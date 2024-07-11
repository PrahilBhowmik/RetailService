package com.example.RetailService.testUtils;

import com.example.RetailService.entity.Transaction;
import com.example.RetailService.utils.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class TestUtility {

    static Random random = new Random();

    public static Product generateRandomProduct(String id){
        return new Product(id,
                "name"+random.nextInt(1000),
                "category"+random.nextInt(100),
                BigDecimal.valueOf(random.nextInt(1000000)/100.0),
                BigDecimal.valueOf(random.nextInt(1000000)/100.0),
                BigDecimal.valueOf(random.nextInt(1000)/1000.0),
                random.nextInt(100),
                "brand"+random.nextInt(100));
    }

    public static Product[] generateProducts(Integer size,String idPrefix){
        Product[] products = new Product[size];
        for(int i = 0; i< size; i++){
            products[i]=generateRandomProduct(idPrefix+(i+1));
        }
        return products;
    }

    public static HashMap<String,Product> generateProductsMap(Integer size, String idPrefix){
        HashMap<String,Product> productHashMap = new HashMap<>();
        for(int i = 1; i<= size; i++){
            productHashMap.put(idPrefix+i,generateRandomProduct(idPrefix+i));
        }
        return productHashMap;
    }

    public static HashMap<String,Product> generateProductsWithFrequentCategory(Integer size, String idPrefix, String category){
        HashMap<String,Product> productHashMap = new HashMap<>();
        for(int i = 1; i<= size; i++){
            Product product = generateRandomProduct(idPrefix+i);
            if(i%2==0){
                product.setCategory(category);
            }
            productHashMap.put(idPrefix+i,product);
        }
        return productHashMap;
    }

    public static HashMap<String,Product> generateProductsWithFrequentBrand(Integer size, String idPrefix, String brand){
        HashMap<String,Product> productHashMap = new HashMap<>();
        for(int i = 1; i<= size; i++){
            Product product = generateRandomProduct(idPrefix+i);
            if(i%2==0){
                product.setBrand(brand);
            }
            productHashMap.put(idPrefix+i,product);
        }
        return productHashMap;
    }

    public static Transaction generateRandomTransaction(String userId, Long fromDate, Long toDate, BigDecimal total, TransactionType type){
        int types = TransactionType.values().length;
        if(total==null){
            total = BigDecimal.valueOf(random.nextInt(100000000)/100.0);
        }
        if(type==null){
            type=TransactionType.values()[random.nextInt(types)];
        }
        return new Transaction(null,
                generateProducts(random.nextInt(15),"PA"),
                type,
                total,
                new Date(random.nextLong(toDate-fromDate)+fromDate),
                userId
        );
    }

    public static Transaction[] generateTransactions(Integer size, String userId, Long fromDate, Long toDate, TransactionsStatus transactionsStatus){
        Transaction[] transactions = new Transaction[size];
        BigDecimal amount = BigDecimal.ZERO;
        for(int i=0;i<size-1;i++){
            transactions[i]= generateRandomTransaction(userId, fromDate, toDate, null,null);
            if(transactions[i].getType()==TransactionType.SELL || transactions[i].getType()==TransactionType.RETURN_BUY){
                amount=amount.add(transactions[i].getTotal());
            }
            else if(transactions[i].getType()==TransactionType.RETURN_SELL || transactions[i].getType()==TransactionType.BUY){
                amount=amount.subtract(transactions[i].getTotal());
            }
        }
        if(transactionsStatus == TransactionsStatus.PROFIT){
            transactions[size-1] = generateRandomTransaction(userId,fromDate,toDate,(amount.multiply(BigDecimal.TWO)).abs(),TransactionType.SELL);
        } else if(transactionsStatus == TransactionsStatus.LOSS){
            transactions[size-1] = generateRandomTransaction(userId,fromDate,toDate,(amount.multiply(BigDecimal.TWO)).abs(),TransactionType.BUY);
        } else if(transactionsStatus == TransactionsStatus.NONE){
            TransactionType type;
            if(amount.signum()>0){
                type=TransactionType.BUY;
            }
            else{
                type=TransactionType.SELL;
            }
            transactions[size-1] = generateRandomTransaction(userId,fromDate,toDate,amount.abs(),type);
        }
        return transactions;
    }

    public static Report analyseTransactions(Transaction[] transactions){
        HashMap<String,BigDecimal> topBrands = new HashMap<>();
        HashMap<String,BigDecimal> topCategories = new HashMap<>();

        BigDecimal totalBuy=BigDecimal.ZERO;
        BigDecimal totalSell=BigDecimal.ZERO;
        BigDecimal totalBuyReturned=BigDecimal.ZERO;
        BigDecimal totalDispose=BigDecimal.ZERO;
        BigDecimal totalSellReturned=BigDecimal.ZERO;

        for (Transaction transaction: transactions){
            if(transaction.getType()==TransactionType.BUY){
                totalBuy=totalBuy.add(transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    Utility.addToMap(topBrands,product.getBrand(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())).negate());
                    Utility.addToMap(topCategories,product.getCategory(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())).negate());
                }
            } else if (transaction.getType()==TransactionType.SELL) {
                totalSell=totalSell.add(transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    Utility.addToMap(topBrands,product.getBrand(),product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits())));
                    Utility.addToMap(topCategories,product.getCategory(),product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits())));
                }
            } else if (transaction.getType()==TransactionType.DISPOSE) {
                totalDispose=totalDispose.add(transaction.getTotal());
            } else if (transaction.getType()==TransactionType.RETURN_BUY){
                totalBuyReturned=totalBuyReturned.add(transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    Utility.addToMap(topBrands,product.getBrand(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
                    Utility.addToMap(topCategories,product.getCategory(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
                }
            }else if (transaction.getType()==TransactionType.RETURN_SELL) {
                totalSellReturned=totalSellReturned.add(transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    Utility.addToMap(topBrands,product.getBrand(),product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits())).negate());
                    Utility.addToMap(topCategories,product.getCategory(),product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits())).negate());
                }
            }
        }
        return new Report(null,null,totalSell.add(totalBuyReturned).subtract(totalSellReturned).subtract(totalBuy),
                totalSell.subtract(totalSellReturned),totalBuy.subtract(totalBuyReturned),null,null,topBrands,
                topCategories,totalBuy,totalSell,totalBuyReturned,totalSellReturned,totalDispose);
    }

}
