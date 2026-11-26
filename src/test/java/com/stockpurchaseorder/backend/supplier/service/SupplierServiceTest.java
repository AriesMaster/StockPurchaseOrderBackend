package com.stockpurchaseorder.backend.supplier.service;
import com.stockpurchaseorder.backend.supplier.persistence.*;
import com.stockpurchaseorder.backend.supplier.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SupplierServiceTest {
    private final SupplierRepository repo = mock(SupplierRepository.class);
    private final SupplierService service = new SupplierService(repo);
    @Test void rejectsInvalidPagination() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> service.list(0,101,null)).getStatusCode());
        verifyNoInteractions(repo);
    }
    @Test void missingSupplierReturnsNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> service.get(1L)).getStatusCode());
    }
    @Test void searchesWithStableSort() {
        var page = PageRequest.of(0,20,Sort.by("name","id"));
        when(repo.findByNameContainingIgnoreCase("hardware",page)).thenReturn(Page.empty(page));
        assertTrue(service.list(0,20," hardware ").isEmpty());
        verify(repo).findByNameContainingIgnoreCase("hardware",page);
    }
    @Test void createsSupplier() {
        when(repo.saveAndFlush(any(SupplierEntity.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals("Hardware",service.create(new SupplierRequest("Hardware",null,null,null,null)).name());
    }
    @Test void updatesAndDeactivatesSupplier() {
        var s = new SupplierEntity("Old",null,null,null,null);
        when(repo.findById(1L)).thenReturn(Optional.of(s));
        when(repo.saveAndFlush(s)).thenReturn(s);
        assertEquals("New",service.update(1L,new SupplierRequest("New",null,null,null,null)).name());
        assertFalse(service.changeStatus(1L,false).active());
        assertTrue(service.changeStatus(1L,true).active());
    }
}
