package com.maracana.repository;

import com.maracana.model.Pago;
import com.maracana.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {
    Optional<Pago> findByReserva(Reserva reserva);
}
