package org.example.dao;

import org.example.entities.Checkout;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DAORobustnessTest {

    @Test
    void testCheckoutDAORobustness() throws Exception {
        CheckoutDAO dao = new CheckoutDAO();
        ResultSet rs = Mockito.mock(ResultSet.class);
        ResultSetMetaData md = Mockito.mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(4);
        
        // Mock columns
        when(md.getColumnName(1)).thenReturn("checkout_id");
        when(md.getColumnName(2)).thenReturn("flight_id");
        when(md.getColumnName(3)).thenReturn("user_id");
        when(md.getColumnName(4)).thenReturn("total_prix");
        
        // Mock values
        when(rs.getInt("checkout_id")).thenReturn(1);
        when(rs.getString("flight_id")).thenReturn("FL123");
        when(rs.getInt("user_id")).thenReturn(42);
        when(rs.getBigDecimal("total_prix")).thenReturn(new BigDecimal("99.99"));
        
        // Use reflection to access private method for testing extraction logic
        Method extractMethod = CheckoutDAO.class.getDeclaredMethod("extractCheckoutFromResultSet", ResultSet.class);
        extractMethod.setAccessible(true);
        
        Checkout checkout = (Checkout) extractMethod.invoke(dao, rs);
        
        assertEquals(42, checkout.getIdUser());
        assertEquals(new BigDecimal("99.99"), checkout.getTotalPrix());
        assertEquals("FL123", checkout.getFlightId());
    }
}
