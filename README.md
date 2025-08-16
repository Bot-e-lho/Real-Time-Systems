## Sistema de Controle de Caldeira em Tempo Real (RTSJ)

Este projeto implementa um sistema de controle para uma caldeira a vapor, utilizando a Real-Time Specification for Java (RTSJ). O objetivo é demonstrar o comportamento de um sistema reativo e seguro que opera em ciclos periódicos, respondendo a diferentes condições e falhas do ambiente físico.

O sistema é dividido em duas partes principais: a simulação do sistema físico (a caldeira e seus componentes) e a lógica do sistema de controle, que gerencia a operação para garantir que o nível da água permaneça dentro dos limites de segurança.


### Componentes:

#### - Leonardo Melo

#### - Miguel Botelho


### O projeto é estruturado em classes que representam os componentes do sistema real:

#### - Boiler: Modela a caldeira, gerenciando a quantidade de água e os fluxos de entrada (bombas) e saída (vapor).

#### - Pump: Simula o comportamento das bombas, incluindo a capacidade de falhar e um atraso na inicialização.

#### - Sensors: Classes internas (LevelSensor e VaporSensor) que simulam a leitura e a falha dos sensores de nível e vapor.

#### - EvacuationValve: Representa a válvula de evacuação, usada para ajustar o nível da água.

#### - Controller: O cérebro do sistema. Ele opera em ciclos periódicos para ler os sensores e comandar os atuadores (bombas e válvula) com base no estado atual da caldeira.

#### - Simulator: Orquestra a execução, criando as threads de tempo real para o sistema físico e o controlador, e injetando falhas para testar a resiliência do sistema.

#### Linhas de comando utilizadas:

javac -d . -cp .:/usr/local/jamaica-8.10-1/target/linux-x86_64/lib/rt.jar boiler/*.java


/usr/local/jamaica-8.10-1/target/linux-x86_64/bin/jamaicavm_bin   -cp .:/usr/local/jamaica-8.10-1/target/linux-x86_64/lib/rt.jar   boiler.Simulator 180
