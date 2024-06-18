package com.example.RetailService.testUtils;

import com.example.RetailService.utils.Product;

import java.util.HashMap;
import java.util.Random;

public class TestUtility {

    static Random random = new Random();

    public static Product generateRandomProduct(String id){
        return new Product(id,
                "name"+random.nextInt(1000),
                "category"+random.nextInt(100),
                random.nextDouble(10000.00),
                random.nextDouble(10000.00),
                random.nextDouble(1.00),
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
}
