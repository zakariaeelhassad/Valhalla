package com.example.backend.service;

import com.example.backend.dto.game.TransferWindowStatusResponse;

public interface TransferWindowService {

    TransferWindowStatusResponse getTransferWindowStatus();

    boolean isTransfersAllowed();
}
