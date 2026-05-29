package com.familyledger;

import com.familyledger.repo.LedgerRepository;
import com.familyledger.service.LedgerService;
import com.familyledger.service.VoiceLedgerService;
import com.familyledger.web.ApiHandler;
import com.familyledger.web.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class LedgerApplication {
    private LedgerApplication() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path publicDir = Path.of("public");

        LedgerRepository repository = new LedgerRepository();
        LedgerService ledgerService = new LedgerService(repository);
        VoiceLedgerService voiceLedgerService = new VoiceLedgerService();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiHandler(ledgerService, voiceLedgerService));
        server.createContext("/", new StaticFileHandler(publicDir));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("家庭记账系统已启动：http://localhost:" + port);
    }
}
