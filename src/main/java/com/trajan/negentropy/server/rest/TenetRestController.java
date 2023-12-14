package com.trajan.negentropy.server.rest;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.Tenet;
import com.trajan.negentropy.server.facade.TenetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenet")
@Slf4j
@Benchmark
public class TenetRestController {
    @Autowired private TenetService tenetService;

    @GetMapping("")
    public Tenet random() {
        return tenetService.getRandom();
    }
}
