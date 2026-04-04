package com.miraquest.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miraquest.api.model.Questao;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

	@Query(value = "SELECT * FROM questao WHERE UPPER(materia) = UPPER(:materia) ORDER BY RANDOM() LIMIT 5", nativeQuery = true)
	List<Questao> findCincoAleatoriasPorMateria(@Param("materia") String materia);

}
