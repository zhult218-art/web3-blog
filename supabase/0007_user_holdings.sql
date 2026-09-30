-- 0007: 用户持仓表
-- 用户可管理自己的持仓数据，管理员可查看所有用户持仓
-- 通过 Python 量化服务 (quant-py-service) 的 REST API 访问

CREATE TABLE IF NOT EXISTS public.user_holdings (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  username    VARCHAR(50),
  stock_code  VARCHAR(20) NOT NULL,
  stock_name  VARCHAR(50),
  shares      INT NOT NULL DEFAULT 0,
  cost_price  DECIMAL(10,3) NOT NULL DEFAULT 0,
  buy_date    DATE,
  notes       TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  updated_at  TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, stock_code)
);

-- 索引：按用户查询
CREATE INDEX IF NOT EXISTS idx_user_holdings_user_id ON public.user_holdings(user_id);
-- 索引：管理员按用户聚合统计
CREATE INDEX IF NOT EXISTS idx_user_holdings_stock_code ON public.user_holdings(stock_code);

COMMENT ON TABLE public.user_holdings IS '用户持仓数据（量化模块）';
COMMENT ON COLUMN public.user_holdings.user_id IS '用户 ID（来自 Java JWT）';
COMMENT ON COLUMN public.user_holdings.stock_code IS '股票代码（如 600519）';
COMMENT ON COLUMN public.user_holdings.shares IS '持仓股数';
COMMENT ON COLUMN public.user_holdings.cost_price IS '成本价';
