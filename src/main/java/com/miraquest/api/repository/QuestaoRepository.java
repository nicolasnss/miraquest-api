package com.miraquest.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miraquest.api.model.Questao;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

	@Query(value = "SELECT * FROM questao WHERE UPPER(materia) = UPPER(:materia) ORDER BY RANDOM() LIMIT 5", nativeQuery = true)
	List<Questao> findCincoAleatoriasPorMateria(@Param("materia") String materia);

	/** Pares [materia, quantidade] exatamente como gravados (o agrupamento sem diferenciar caixa é no service). */
	@Query("select q.materia, count(q) from Questao q where q.materia is not null and trim(q.materia) <> '' group by q.materia")
	List<Object[]> contarPorMateria();

	boolean existsByMateriaIgnoreCaseAndEnunciado(String materia, String enunciado);

}
