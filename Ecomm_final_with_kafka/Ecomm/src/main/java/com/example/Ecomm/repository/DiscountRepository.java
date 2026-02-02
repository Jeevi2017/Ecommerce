	package com.example.Ecomm.repository;
	
	import java.util.List;
import java.util.Optional;
	
	import org.springframework.data.jpa.repository.JpaRepository;
	import org.springframework.stereotype.Repository;
	
	import com.example.Ecomm.entitiy.Discount;
	
	@Repository
	public interface DiscountRepository extends JpaRepository<Discount, Long>{
	
		Optional<Discount> findByCode(String code);

		List<Discount> findByActiveTrue();
	
		
	
		
	}
