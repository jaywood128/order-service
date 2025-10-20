package com.streamcart.order.seeder;

import com.streamcart.order.entity.Product;
import com.streamcart.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds initial product data on application startup.
 * Only runs if products table is empty (idempotent).
 * 
 * Order(1) ensures this runs before other seeders.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ProductDataSeeder implements CommandLineRunner {
    
    private final ProductRepository productRepository;
    
    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            log.info("🌱 Seeding initial product catalog (The Office Edition)...");
            seedProducts();
            log.info("✅ Product catalog seeded successfully! {} products available.", 
                    productRepository.count());
        } else {
            log.info("📦 Product catalog already exists ({} products). Skipping seed.", 
                    productRepository.count());
        }
    }
    
    private void seedProducts() {
        List<Product> products = List.of(
            // Paper Products - The Core Business
            createProduct(
                "DM-PAPER-001",
                "Dunder Mifflin Paper - Premium White",
                new BigDecimal("6.99"),
                "High-quality office paper, 500 sheets. The best paper in Scranton!",
                5000
            ),
            createProduct(
                "DM-PAPER-002",
                "Dunder Mifflin Paper - Recycled",
                new BigDecimal("5.99"),
                "Eco-friendly recycled paper, 500 sheets. Save the rainforest!",
                3000
            ),
            createProduct(
                "DM-PAPER-003",
                "Dunder Mifflin Colored Paper Pack",
                new BigDecimal("8.99"),
                "Assorted colors for creative projects. Perfect for party planning!",
                1500
            ),
            createProduct(
                "DM-PAPER-004",
                "Cardstock - Heavy Duty",
                new BigDecimal("12.99"),
                "Premium cardstock, 250 sheets. Great for invitations!",
                800
            ),
            
            // Office Supplies
            createProduct(
                "DM-SUPPLY-001",
                "Stapler - Red Swingline",
                new BigDecimal("15.99"),
                "Premium desktop stapler. Milton approved. Do NOT put in Jell-O.",
                50
            ),
            createProduct(
                "DM-SUPPLY-002",
                "Three-Hole Punch",
                new BigDecimal("24.99"),
                "Industrial strength hole punch. Not a murder weapon.",
                35
            ),
            createProduct(
                "DM-SUPPLY-003",
                "Binder Clips - Assorted Sizes",
                new BigDecimal("7.99"),
                "Box of 50 clips. Keep your documents organized!",
                200
            ),
            createProduct(
                "DM-SUPPLY-004",
                "Sticky Notes - Yellow 3x3",
                new BigDecimal("4.99"),
                "Classic yellow sticky notes, 12 pads. Leave passive-aggressive notes!",
                500
            ),
            
            // The Office Memorabilia
            createProduct(
                "DM-MERCH-001",
                "World's Best Boss Mug",
                new BigDecimal("12.99"),
                "Ceramic coffee mug. Perfect for regional managers!",
                100
            ),
            createProduct(
                "DM-MERCH-002",
                "Dundie Award Trophy",
                new BigDecimal("19.99"),
                "Employee recognition award. Best Salesman, Whitest Sneakers, you name it!",
                200
            ),
            createProduct(
                "DM-MERCH-003",
                "Teapot - Ceramic",
                new BigDecimal("24.99"),
                "Beautiful ceramic teapot. May contain secret notes from admirers.",
                10
            ),
            createProduct(
                "DM-MERCH-004",
                "Dwight Schrute Bobblehead",
                new BigDecimal("29.99"),
                "Assistant (to the) Regional Manager bobblehead. Bears, beets, Battlestar Galactica.",
                30
            ),
            createProduct(
                "DM-MERCH-005",
                "Pretzel Day Calendar",
                new BigDecimal("14.99"),
                "365 days of pretzel appreciation. Stanley approved.",
                40
            ),
            
            // Desk Accessories
            createProduct(
                "DM-DESK-001",
                "Office Desk Nameplate - Brass",
                new BigDecimal("9.99"),
                "Customizable brass nameplate. Look professional!",
                75
            ),
            createProduct(
                "DM-DESK-002",
                "Desk Organizer - Wood",
                new BigDecimal("34.99"),
                "Elegant wooden desk organizer. Keep your desk tidy!",
                45
            ),
            createProduct(
                "DM-DESK-003",
                "Mouse Pad - Ergonomic",
                new BigDecimal("11.99"),
                "Gel wrist rest mouse pad. Prevent carpal tunnel!",
                120
            ),
            
            // Party Planning Supplies
            createProduct(
                "DM-PARTY-001",
                "Birthday Party Kit",
                new BigDecimal("49.99"),
                "Complete party kit: balloons, streamers, plates. Party Planning Committee approved!",
                25
            ),
            createProduct(
                "DM-PARTY-002",
                "Streamers - Assorted Colors",
                new BigDecimal("6.99"),
                "Colorful streamers for any occasion. Angela's favorite!",
                80
            ),
            createProduct(
                "DM-PARTY-003",
                "Paper Plates - 100 count",
                new BigDecimal("8.99"),
                "Disposable paper plates. Cleanup is a breeze!",
                150
            ),
            
            // Specialty Items
            createProduct(
                "DM-SPECIAL-001",
                "Survival Kit - Office Edition",
                new BigDecimal("79.99"),
                "Everything you need to survive a lockdown. Dwight's design.",
                15
            ),
            createProduct(
                "DM-SPECIAL-002",
                "Conference Room Whiteboard",
                new BigDecimal("149.99"),
                "4x6 foot whiteboard. Perfect for important meetings!",
                8
            ),
            createProduct(
                "DM-SPECIAL-003",
                "Neon 'It Is Your Birthday' Sign",
                new BigDecimal("39.99"),
                "Not 'It's' but 'It Is'. Brown and grey balloons not included.",
                20
            )
        );
        
        productRepository.saveAll(products);
    }
    
    private Product createProduct(String id, String name, BigDecimal price, 
                                 String description, Integer stockQuantity) {
        return Product.builder()
                .productId(id)
                .name(name)
                .price(price)
                .description(description)
                .stockQuantity(stockQuantity)
                .build();
    }
}

