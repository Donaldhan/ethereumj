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
package org.ethereum.facade;

/**
 * Represents the current state of syncing process
 */
public class SyncStatus {
    public enum SyncStage {
        /**
         * Fast sync: looking for a Pivot block.
         * Normally we need several peers to select the block but
         * the block can be selected from existing peers due to timeout、
         * 快速同步：寻找基准区块
         */
        PivotBlock,
        /**
         * Fast sync: downloading state trie nodes and importing blocks
         * 下载状态树节点，并导入区块
         */
        StateNodes,
        /**
         * Fast sync: downloading headers for securing the latest state
         * 下载最新获取状态的区块头
         */
        Headers,
        /**
         * Fast sync: downloading blocks
         * 下载区块
         */
        BlockBodies,
        /**
         * Fast sync: downloading receipts
         * 下载交易回执
         */
        Receipts,
        /**
         * Regular sync is in progress
         */
        Regular,
        /**
         * Sync is complete:
         * Fast sync: the state is secure, all blocks and receipt are downloaded
         * Regular sync: all blocks are imported up to the blockchain head
         * 同步完成；
         * Fast sync：状态可靠的，所有区块和回执已下载；
         * Regular sync：所有区块导入到区块链head
         */
        Complete,
        /**
         * Syncing is turned off
         * 同步关闭
         */
        Off;

        /**
         * Indicates if this state represents ongoing FastSync
         */
        public boolean isFastSync() {
            return this == PivotBlock || this == StateNodes || this == Headers || this == BlockBodies || this == Receipts;
        }

        /**
         * Indicates the current state is secure
         * 当前状态是否为可靠的
         * When doing fast sync UNSECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * but the state isn't yet confirmed with  the whole block chain and can't be
         * trusted.
         * At this stage historical blocks and receipts are unavailable yet
         *
         * 当做快速的非安全同步是，意味着下载所有状态，在链上的最后的状态，和区块链操作将会别执行（比如状态查询，交易提交）；
         * 但是，状态还没有被正而过区块链确认，不可信。
         * 在此阶段，历史区块和回执是不可用的；
         *
         * SECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * The state is now confirmed by the full chain (all block headers are
         * downloaded and verified) and can be trusted
         * At this stage historical blocks and receipts are unavailable yet
         * 安全同步意味着全链被确认（所有区块头部已下载，并验证），并且可信；
         * 在此阶段，历史区块和回执是不可用的；
         */
        public boolean isSecure() {
            return this != PivotBlock || this != StateNodes && this != Headers;
        }

        /**
         * Indicates the blockchain state is up-to-date
         * Warning: the state could still be non-secure
         */
        public boolean hasLatestState() {
            return this == Headers || this == BlockBodies || this == Receipts || this == Complete;
        }
    }

    /**
     * 同步阶段
     */
    private final SyncStage stage;
    /**
     * 当前阶段处理的item数量
     */
    private final long curCnt;
    /**
     * 当前阶段已知的item数量
     */
    private final long knownCnt;
    /**
     * 上次导入的区块
     */
    private final long blockLastImported;
    /**
     * 最优区块
     */
    private final long blockBestKnown;

    public SyncStatus(SyncStatus state, long blockLastImported, long blockBestKnown) {
        this(state.getStage(), state.getCurCnt(), state.getKnownCnt(), blockLastImported, blockBestKnown);
    }

    public SyncStatus(SyncStage stage, long curCnt, long knownCnt, long blockLastImported, long blockBestKnown) {
        this.stage = stage;
        this.curCnt = curCnt;
        this.knownCnt = knownCnt;
        this.blockLastImported = blockLastImported;
        this.blockBestKnown = blockBestKnown;
    }

    public SyncStatus(SyncStage stage, long curCnt, long knownCnt) {
        this(stage, curCnt, knownCnt, 0, 0);
    }

    /**
     * Gets the current stage of syncing
     */
    public SyncStage getStage() {
        return stage;
    }

    /**
     * Gets the current count of items processed for this syncing stage :
     * 获取当前同步阶段，处理的items数量
     * PivotBlock: number of seconds pivot block is searching for
     *          ( this number can be greater than getKnownCnt() if no peers found)
     *
     *          搜索基准区块的数量
     * StateNodes: number of trie nodes downloaded
     *  mpt树节点的数量
     * Headers: number of headers downloaded
     * 区块头数量
     * BlockBodies: number of block bodies downloaded
     * 区块体数量
     * Receipts: number of blocks receipts are downloaded for
     * 区块回执数量
     */
    public long getCurCnt() {
        return curCnt;
    }

    /**
     * Gets the known count of items for this syncing stage :
     * 当前同步阶段，已知的item数量
     * PivotBlock: number of seconds pivot is forced to be selected
     * 将被选择的区块数量
     * StateNodes: number of currently known trie nodes. This number is not constant as new nodes
     *             are discovered when their parent is downloaded
     * 已知的mpt树节点的数量
     * Headers: number of headers to be downloaded
     * 区块头数量
     * BlockBodies: number of block bodies to be downloaded
     * 区块体数量
     * Receipts: number of blocks receipts are to be downloaded for
     * 区块回执数量
     */
    public long getKnownCnt() {
        return knownCnt;
    }

    /**
     * Reflects the blockchain state: the latest imported block
     * Blocks importing can run in parallel with other sync stages
     * (like header/blocks/receipts downloading)
     * 上次导入的区块
     */
    public long getBlockLastImported() {
        return blockLastImported;
    }

    /**
     * Return the best known block from other peers
     */
    public long getBlockBestKnown() {
        return blockBestKnown;
    }

    @Override
    public String toString() {
        return stage +
                (stage != SyncStage.Off && stage != SyncStage.Complete ? " (" + getCurCnt() + " of " + getKnownCnt() + ")" : "") +
                ", last block #" + getBlockLastImported() + ", best known #" + getBlockBestKnown();
    }
}
