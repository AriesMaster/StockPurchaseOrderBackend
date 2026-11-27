package com.stockpurchaseorder.backend.supplier.catalogue;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.Optional;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct,Long> {
    Optional<SupplierProduct> findBySupplier_IdAndProduct_Sku(Long supplierId,String sku);
    @Query("""
        select sp from SupplierProduct sp where sp.supplier.id=:supplierId
        and (:available=false or (sp.active=true and sp.product.active=true and sp.supplier.active=true))
        and (lower(sp.product.name) like lower(concat('%',:search,'%')) or lower(sp.product.sku) like lower(concat('%',:search,'%')))
        order by sp.product.name, sp.product.sku
        """)
    Page<SupplierProduct> catalogue(@Param("supplierId") long supplierId,@Param("available") boolean available,
        @Param("search") String search,Pageable pageable);
}
