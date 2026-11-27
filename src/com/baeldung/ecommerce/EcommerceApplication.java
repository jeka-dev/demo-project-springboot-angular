package com.baeldung.ecommerce;

import com.baeldung.ecommerce.model.Product;
import com.baeldung.ecommerce.service.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(ProductService productService) {
        return args -> {
            productService.save(new Product(null, "TV Set", 300.00, "http://placehold.it/200x100"));
            productService.save(new Product(null, "Game Console", 200.00, "http://placehold.it/200x100"));
            productService.save(new Product(null, "Sofa", 100.00, "http://placehold.it/200x100"));
            productService.save(new Product(null, "Icecream", 5.00, "http://placehold.it/200x100"));
            productService.save(new Product(null, "Beer", 3.00, "http://placehold.it/200x100"));
            productService.save(new Product(null, "Phone", 500.00, "http://placehold.it/200x100"));
            productService.save(new Product(null, "Watch", 30.00, "http://placehold.it/200x100"));
        };
    }
}
