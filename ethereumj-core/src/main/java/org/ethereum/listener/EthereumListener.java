/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.listener;

import org.ethereum.core.*;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.message.Message;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.Channel;

import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 27.07.2014
 */
public interface EthereumListener {

    /**
     * 待确认交易状态
     */
    enum PendingTransactionState {
        /**
         * Transaction may be dropped due to:
         * - Invalid transaction (invalid nonce, low gas price, insufficient account funds,
         *         invalid signature)
         * - Timeout (when pending transaction is not included to any block for
         *         last [transaction.outdated.threshold] blocks
         * This is the final state
         * 交易丢弃状态：
         * 1. 无效的交易（无效的nonce，低gas，账户余额不足，无效的签名）；
         * 2. 超时（待确认交易不在任何区块中前[transaction.outdated.threshold]个区块中）
         *
         */
        DROPPED,

        /**
         * The same as PENDING when transaction is just arrived
         * Next state can be either PENDING or INCLUDED
         * 待确认交易的新建状态，下一步Pending或者included状态
         */
        NEW_PENDING,

        /**
         * State when transaction is not included to any blocks (on the main chain), and
         * was executed on the last best block. The repository state is reflected in the PendingState
         * Next state can be either INCLUDED, DROPPED (due to timeout)
         * or again PENDING when a new block (without this transaction) arrives
         * 当交易不包含在任何区块中（主链），和在last best区块中执行的状态；
         * 下一个状态可以为INCLUDED, DROPPED (due to timeout)或者在没有当前交易的区块到达，再次PENDING
         */
        PENDING,

        /**
         * State when the transaction is included to a block.
         * This could be the final state, however next state could also be
         * PENDING: when a fork became the main chain but doesn't include this tx
         * INCLUDED: when a fork became the main chain and tx is included into another
         *           block from the new main chain
         * DROPPED: If switched to a new (long enough) main chain without this Tx
         *
         * 当前交易包含在当前区块中；此为状态可以为终态，然后下一个状态也可以为：
         * PENDING状态：当fork成为主链，但不包括此交易
         * INCLUDED状态：：当fork成为主链，包含在另一个主链的区块中
         * DROPPED状态:  如果转为一个新的主链，但是没有当前交易
         */
        INCLUDED;

        public boolean isPending() {
            return this == NEW_PENDING || this == PENDING;
        }
    }

    /**
     *
     */
    enum SyncState {
        /**
         * When doing fast sync UNSECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * but the state isn't yet confirmed with  the whole block chain and can't be
         * trusted.
         * At this stage historical blocks and receipts are unavailable yet
         * 当前正在做快速的非安全同步时（下载全状态），链在最后一个区块，同时区块链操作正在执行（比如状态查询，交易提交），
         * 但是区块链还没有确认状态，当前世界状态不可信。
         * 在此阶段，历史区块和回执还不可用；
         *
         */
        UNSECURE,
        /**
         * When doing fast sync SECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * The state is now confirmed by the full chain (all block headers are
         * downloaded and verified) and can be trusted
         * At this stage historical blocks and receipts are unavailable yet
         *
         * 当前正在做快速的安全同步时（下载全状态），链在最后一个区块，同时区块链操作正在执行（比如状态查询，交易提交），
         * 但是区块链确认状态，当前世界状态可信。
         * 在此阶段，历史区块和回执还不可用；
         *
         */
        SECURE,
        /**
         * Sync is fully complete. All blocks and receipts are downloaded.
         * 同步完成，所有的区块和回执已经下载完毕
         */
        COMPLETE
    }

    void trace(String output);

    /**
     * @param node
     */
    void onNodeDiscovered(Node node);

    /**
     * @param channel
     * @param helloMessage
     */
    void onHandShakePeer(Channel channel, HelloMessage helloMessage);

    /**
     * @param channel
     * @param status
     */
    void onEthStatusUpdated(Channel channel, StatusMessage status);

    /**
     * @param channel
     * @param message
     */
    void onRecvMessage(Channel channel, Message message);

    /**
     * @param channel
     * @param message
     */
    void onSendMessage(Channel channel, Message message);

    /**
     * @param blockSummary
     */
    void onBlock(BlockSummary blockSummary);

    /**
     * @param blockSummary
     * @param best
     */
    default void onBlock(BlockSummary blockSummary, boolean best) {
        onBlock(blockSummary);
    }

    /**
     * @param host
     * @param port
     */
    void onPeerDisconnect(String host, long port);

    /**
     * @deprecated use onPendingTransactionUpdate filtering state NEW_PENDING
     * Will be removed in the next release
     */
    void onPendingTransactionsReceived(List<Transaction> transactions);

    /**
     * PendingState changes on either new pending transaction or new best block receive
     * When a new transaction arrives it is executed on top of the current pending state
     * When a new best block arrives the PendingState is adjusted to the new Repository state
     * and all transactions which remain pending are executed on top of the new PendingState
     *
     * 在新的交易或new best 区块接收时，PendingState变更；
     * 当新的交易到达时，在当前pending状态下执行；
     * 当前新的区块到pending状态（刚刚调整为新的Repository状态），并且所有的交易，在新的PendingState下，
     * pending执行；
     *
     */
    void onPendingStateChanged(PendingState pendingState);

    /**
     * Is called when PendingTransaction arrives, executed or dropped and included to a block
     * 当待确认的交易到达，执行，丢弃或者包含在一个区块中，调用
     * @param txReceipt Receipt of the tx execution on the current PendingState 待确认状态的交易回执
     * @param state Current state of pending tx 待确认交易状态
     * @param block The block which the current pending state is based on (for PENDING tx state)
     *              or the block which tx was included to (for INCLUDED state) 当前区块
     */
    void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block);

    void onSyncDone(SyncState state);

    void onNoConnections();

    void onVMTraceCreated(String transactionHash, String trace);

    void onTransactionExecuted(TransactionExecutionSummary summary);

    void onPeerAddedToSyncPool(Channel peer);
}
