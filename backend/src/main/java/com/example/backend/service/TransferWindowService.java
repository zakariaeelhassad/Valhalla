package com.example.backend.service;

public interface TransferWindowService {

    com.example.backend.service.Impl.TransferWindowService.TransferWindowStatus getTransferWindowStatus();

    boolean isTransfersAllowed();
}
