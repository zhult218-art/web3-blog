package com.web3.jarvis.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置类(jarvis-service 模块)。
 *
 * <p>{@code @Configuration} 声明为配置类。定义 Jarvis 语音指令消息的
 * 主题交换机(topic exchange)、队列与绑定关系:交换机名为 jarvis.exchange
 * (持久化),队列 jarvis.command(持久化),路由键 jarvis.command。
 * JarvisService 通过 AmqpTemplate 将匹配到的语音指令发布到该队列,
 * 供下游消费者执行具体动作。</p>
 */
@Configuration
public class RabbitConfig {
    /** 交换机名称 */
    public static final String EXCHANGE = "jarvis.exchange";
    /** 队列名称 */
    public static final String QUEUE = "jarvis.command";
    /** 路由键 */
    public static final String ROUTING_KEY = "jarvis.command";

    /**
     * 创建主题交换机(持久化、不自动删除)。
     *
     * @return TopicExchange 实例
     */
    @Bean
    public TopicExchange jarvisExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * 创建指令队列(持久化)。
     *
     * @return Queue 实例
     */
    @Bean
    public Queue jarvisQueue() {
        return new Queue(QUEUE, true);
    }

    /**
     * 将队列绑定到交换机,使用 jarvis.command 路由键。
     *
     * @param jarvisQueue   指令队列
     * @param jarvisExchange 主题交换机
     * @return 绑定关系实例
     */
    @Bean
    public org.springframework.amqp.core.Binding jarvisBinding(Queue jarvisQueue, TopicExchange jarvisExchange) {
        return BindingBuilder.bind(jarvisQueue).to(jarvisExchange).with(ROUTING_KEY);
    }
}