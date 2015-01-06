FROM centos

RUN yum -y install java-1.7.0-openjdk-devel.x86_64
RUN yum -y install maven

ADD ./ /

RUN mvn install
