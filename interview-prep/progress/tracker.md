# 面试考点追踪 · 徐率航

> 更新规则：每次 session 结束后由 Claude Code 自动更新
> 状态：✅ 掌握 / ⚠️ 需加强 / ❌ 薄弱 / ⬜ 未练习

---

## 总体进度

| 方向 | 已掌握 | 需加强 | 薄弱/未练习 | 就绪率 |
|------|--------|--------|-------------|--------|
| Java 后端 | 0 | 0 | 22（1 题已测：❌） | 0% |
| AI 工程化 | 0 | 0 | 14 | 0% |

---

## 方向 A：Java 后端

### 项目深挖
| 考点 | 状态 | 备注 |
|------|------|------|
| MCT：flowsvr / worker 两层架构设计思路 | ❌ | 停留在"解耦"口号，无法展开职责边界和通信机制 |
| MCT：MySQL + Redis 二级存储，为什么这么分 | ⬜ | |
| MCT：冷热数据分离的具体实现 | ⬜ | |
| MCT：Redis ZSET 时间分片方案细节 | ⬜ | |
| MCT：从 MySQL 行锁迁移到 Redis 分布式锁的过程 | ⬜ | |
| MCT：wrk 压测 → 500 QPS 到 2000 QPS，每一步优化了什么 | ⬜ | |
| AlarmTask：三层微服务拆分原则 | ⬜ | |
| AgentSmartDesk：整体架构介绍（1 分钟版本） | ⬜ | |

### MySQL
| 考点 | 状态 | 备注 |
|------|------|------|
| B+ 树索引结构，为什么不用 B 树 | ⬜ | |
| 聚簇索引 vs 非聚簇索引 | ⬜ | |
| 覆盖索引、索引下推 | ⬜ | |
| 事务四大特性 ACID | ⬜ | |
| 四种隔离级别 + 对应问题 | ⬜ | |
| MVCC 原理（undo log、版本链、ReadView） | ⬜ | |
| 慢查询定位与优化 | ⬜ | |
| 分表策略（按任务大小分表） | ⬜ | |

### Redis
| 考点 | 状态 | 备注 |
|------|------|------|
| ZSET 底层：跳表 vs ziplist，什么时候转换 | ⬜ | |
| 分布式锁：SETNX + expire 的问题，Redisson 怎么解决 | ⬜ | |
| 缓存穿透 / 击穿 / 雪崩的区别和解决方案 | ⬜ | |
| Redis 持久化：RDB vs AOF | ⬜ | |
| Redis 过期策略与内存淘汰策略 | ⬜ | |

### Java 并发
| 考点 | 状态 | 备注 |
|------|------|------|
| 线程池七大参数，拒绝策略 | ⬜ | |
| synchronized vs ReentrantLock | ⬜ | |
| AQS 原理 | ⬜ | |
| volatile 原理（内存可见性、禁止重排序） | ⬜ | |

---

## 方向 B：AI 工程化

### AgentSmartDesk 项目深挖
| 考点 | 状态 | 备注 |
|------|------|------|
| 多 Agent 编排架构设计，为什么选 Orchestrator 模式 | ⬜ | |
| 意图分类前置的实现细节，6 类意图怎么定义 | ⬜ | |
| SummaryBufferChatMemory：阈值怎么定，摘要 prompt 怎么写 | ⬜ | |
| RAG 管道：分块策略（滑动窗口 + overlap），chunk size 怎么选 | ⬜ | |
| RAG 管道：embedding 模型选型（text-embedding-v3） | ⬜ | |
| RAG 管道：召回后 rerank 有没有做 | ⬜ | |
| 并行路由多个子 Agent 怎么实现，结果怎么汇总 | ⬜ | |

### LLM 工程基础
| 考点 | 状态 | 备注 |
|------|------|------|
| Tool Calling 原理（Function Calling 流程） | ⬜ | |
| Prompt 设计原则（System Prompt vs User Prompt） | ⬜ | |
| Context 窗口管理策略（截断 vs 摘要 vs 压缩） | ⬜ | |
| Token 计算与费用优化 | ⬜ | |
| ReAct loop 原理（Thought → Action → Observation） | ⬜ | |

### 模型推理基础（AI Infra 加分项）
| 考点 | 状态 | 备注 |
|------|------|------|
| KV Cache 原理（类比 Redis 缓存） | ⬜ | |
| Batching 策略（static vs dynamic batching） | ⬜ | |
| 量化基础（INT8/INT4，精度损失） | ⬜ | |

---

## 薄弱点记录（由 CC 自动追加）

- **MCT flowsvr/worker 职责边界**：只说了"解耦"，无法描述各层具体职责和通信机制（2026-03-20）

---

## Session 历史

| 日期 | 模式 | 题目数 | 平均分 | 新发现薄弱点 |
|------|------|--------|--------|--------------|
| 2026-03-20 | A | 1 | 1/5 | MCT 架构职责边界不清 |
