package com.adobe.training.core.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Tests the Stockplex model using the Mockito testing framework.
 * This is good for simple test cases, where the stubbing is fairly simple like in this example.
 * For more complex mocking, use the Sling Mock API or the AEM Mocks when AEM objects need to be mocked.
 * 
 * Note that the testable class is under /src/main/java:
 * com.adobe.training.core.models.Stockplex.java
 * 
 *  To correctly use this testing class:
 *  -put this file under training.core/src/test/java in the package com.adobe.training.core.models
 * 
 */
@ExtendWith(MockitoExtension.class)
public class StockplexMockitoTest {
 
	private Stockplex stock;
	
    @BeforeEach
    public void setup() throws Exception {
    	
    	//Adapt the Resource if needed
    	Stockplex STOCKMODEL_MOCK = mock(Stockplex.class);
    	
    	stock = STOCKMODEL_MOCK;
    	
    	//Setup stock symbol
    	final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final int N = alphabet.length();
        Random r = new Random();
        String str = "";
        for (int i = 0; i < 4; i++) {
            str = str + alphabet.charAt(r.nextInt(N));
        }
    	when(STOCKMODEL_MOCK.getSymbol()).thenReturn(str);
    	
    	//Setup current trade price
    	Random rand = new Random();
    	double n = Math.round(100*(rand.nextInt(150) + 100)+rand.nextDouble())/100; //random value between 100.00 and 150.00
    	when(STOCKMODEL_MOCK.getCurrentPrice()).thenReturn(n);

    }
    
	@Test
	void testGetLastTradeValue() throws Exception{
		assertNotNull(stock, "lastTradeModel is null");
		assertTrue(stock.getCurrentPrice() > 100, "current value is incorrect");
		assertFalse(stock.getSymbol().isEmpty(), "stock symbol is incorrect");
		
		// Verify that only those two methods were called on the mocked object
		verify(stock).getCurrentPrice();
		verify(stock).getSymbol();
		
		// Verify that no other interaction occurred on the mocked object
		verifyNoMoreInteractions(stock);
		
		// The one below is when we want to make sure that a specific method was never invoked 
		verify(stock, never()).getSummary();
	}
}