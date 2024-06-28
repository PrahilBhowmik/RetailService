package com.example.RetailService.utils;

import com.example.RetailService.entity.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Utility {
    private static void addToMap(HashMap<String,Double> hashMap,String key, Double value){
        if(hashMap.containsKey(key)){
            hashMap.put(key,hashMap.get(key)+value);
        }
        else{
            hashMap.put(key,value);
        }
    }
    public static Mono<Report> generateReport(String userId, Date fromDate, Date toDate, Flux<Transaction> transactionFlux){
        AtomicReference<Double> totalBuy= new AtomicReference<>(0.00);
        AtomicReference<Double> totalSell= new AtomicReference<>(0.00);
        AtomicReference<Double> totalBuyReturned= new AtomicReference<>(0.00);
        AtomicReference<Double> totalSellReturned= new AtomicReference<>(0.00);
        AtomicReference<Double> totalDispose= new AtomicReference<>(0.00);
        HashMap<String,Double> topBrands = new HashMap<>();
        HashMap<String,Double> topCategories = new HashMap<>();

        return transactionFlux.map(transaction -> {
            if(transaction.getType()==TransactionType.BUY){
                totalBuy.updateAndGet(v -> v + transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),-product.getCost()*product.getUnits());
                    addToMap(topCategories,product.getCategory(),-product.getCost()*product.getUnits());
                }
            } else if (transaction.getType()==TransactionType.SELL) {
                totalSell.updateAndGet(v -> v + transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),product.getMrp()*product.getDiscount()*product.getUnits());
                    addToMap(topCategories,product.getCategory(),product.getMrp()*product.getDiscount()*product.getUnits());
                }
            } else if (transaction.getType()==TransactionType.RETURN_BUY){
                totalBuyReturned.updateAndGet(v -> v + transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),product.getCost()*product.getUnits());
                    addToMap(topCategories,product.getCategory(),product.getCost()*product.getUnits());
                }
            }else if(transaction.getType()==TransactionType.DISPOSE) {
                totalDispose.updateAndGet(v -> v + transaction.getTotal());
            } else if (transaction.getType()==TransactionType.RETURN_SELL) {
                totalSellReturned.updateAndGet(v -> v + transaction.getTotal());
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),-product.getMrp()*product.getDiscount()*product.getUnits());
                    addToMap(topCategories,product.getCategory(),-product.getMrp()*product.getDiscount()*product.getUnits());
                }
            }
            return transaction;
        }).then(Mono.just(new Report(userId,
                (totalSell.get()-totalSellReturned.get())-(totalBuy.get()-totalBuyReturned.get())>0?"Profit":(totalSell.get()-totalSellReturned.get())-(totalBuy.get()-totalBuyReturned.get())<0?"Loss":"Zero gain/loss",
                (totalSell.get()-totalSellReturned.get())-(totalBuy.get()-totalBuyReturned.get()),
                totalSell.get()-totalSellReturned.get(),
                totalBuy.get()-totalBuyReturned.get(),
                fromDate,toDate, topBrands, topCategories,
                totalBuy.get(),totalSell.get(),totalBuyReturned.get(),
                totalSellReturned.get(),totalDispose.get())));
    }

}
