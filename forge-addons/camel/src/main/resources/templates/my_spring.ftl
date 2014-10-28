<?xml version="1.0" encoding="UTF-8"?>

<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:camel="http://camel.apache.org/schema/spring"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!--
        import the component from another XML file:
        <import resource="this_file.xml"/>
     -->

    <bean id="${componentId}" class="${componentClass}">
      <!-- optional configuration by user goes here... -->
      <!-- <property name="bar">cheese</property> -->
      </bean>

</beans>
