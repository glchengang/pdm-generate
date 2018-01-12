package tools.pdmgenerate;

/**
 *  BASE = 全自动生成覆盖,不手工改动的SQL, 大部分是一个经典简单的增删改查命令
 *  EXTEND = 开发常需要修改和扩展的SQL
 */
public enum IbatisType {
	BASE, EXTEND
}
