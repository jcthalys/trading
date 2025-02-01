package com.trading.repository;

import com.trading.entity.CompositeOrder;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompositeOrderRepository extends JpaRepository<CompositeOrder, UUID> {}
