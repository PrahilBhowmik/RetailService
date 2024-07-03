package com.example.RetailService.utils;

import com.example.RetailService.entity.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

public class Utility {
    public static void addToMap(HashMap<String, BigDecimal> hashMap, String key, BigDecimal value){
        if(hashMap.containsKey(key)){
            hashMap.put(key,hashMap.get(key).add(value));
        }
        else{
            hashMap.put(key,value);
        }
    }
    public static Mono<Report> generateReport(String userId, Date fromDate, Date toDate, Flux<Transaction> transactionFlux){
        HashMap<String, BigDecimal> topBrands = new HashMap<>();
        HashMap<String,BigDecimal> topCategories = new HashMap<>();

        Report report = new Report(userId, null, BigDecimal.valueOf(0),BigDecimal.valueOf(0),BigDecimal.valueOf(0), fromDate,toDate, topBrands, topCategories,BigDecimal.valueOf(0),BigDecimal.valueOf(0),BigDecimal.valueOf(0),BigDecimal.valueOf(0),BigDecimal.valueOf(0));

        return transactionFlux.map(transaction -> {
            if(transaction.getType()==TransactionType.BUY){
                report.setTotalBuy(report.getTotalBuy().add(transaction.getTotal()));
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())).negate());
                    addToMap(topCategories,product.getCategory(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())).negate());
                }
            } else if (transaction.getType()==TransactionType.SELL) {
                report.setTotalSell(report.getTotalSell().add(transaction.getTotal()));
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),product.getMrp().multiply(product.getDiscount()).multiply(BigDecimal.valueOf(product.getUnits())));
                    addToMap(topCategories,product.getCategory(),product.getMrp().multiply(product.getDiscount()).multiply(BigDecimal.valueOf(product.getUnits())));
                }
            } else if (transaction.getType()==TransactionType.DISPOSE) {
                report.setTotalDispose(report.getTotalDispose().add(transaction.getTotal()));
            } else if (transaction.getType()==TransactionType.RETURN_BUY){
                report.setTotalBuyReturned(report.getTotalBuyReturned().add(transaction.getTotal()));
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
                    addToMap(topCategories,product.getCategory(),product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
                }
            }else if (transaction.getType()==TransactionType.RETURN_SELL) {
                report.setTotalSellReturned(report.getTotalSellReturned().add(transaction.getTotal()));
                for(Product product: transaction.getProducts()){
                    addToMap(topBrands,product.getBrand(),product.getMrp().multiply(product.getDiscount()).multiply(BigDecimal.valueOf(product.getUnits())).negate());
                    addToMap(topCategories,product.getCategory(),product.getMrp().multiply(product.getDiscount()).multiply(BigDecimal.valueOf(product.getUnits())).negate());
                }
            }
            report.setIncome(report.getTotalSell().subtract(report.getTotalSellReturned()));
            report.setExpenditure(report.getTotalBuy().subtract(report.getTotalBuyReturned()));
            report.setProfitOrLossAmount(report.getIncome().subtract(report.getExpenditure()));
            report.setStatus(report.getProfitOrLossAmount().signum()>0?TransactionsStatus.PROFIT:report.getProfitOrLossAmount().signum()<0?TransactionsStatus.LOSS:TransactionsStatus.NONE);
            return transaction;
        }).then(Mono.just(report));
    }

}
