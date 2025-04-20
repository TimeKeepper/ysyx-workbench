For 专业综合设计
### 第一周
- 解决流水线的RAW Hazard和Control Hazard
- 摸鱼

### 第二周
- 解决流水线的LS Hazard
- 解决一些边缘情况下存在的小问题，成功运行了流水线

#### TODO
- 以转发而非阻塞解决RAW Hazard
- 优化流水线寄存器状态机转换
- 削减最长路径以增加主频
- 获取性能参数，分析流水线带来的性能提升

### 第三周
由于将cpu改造成流水线，取指和访存会同时发生——并非不能仿真，但是出现了很多小毛病

比如
* 指令追踪缺失，无法进行函数堆栈追踪
* 无法进行Cache的Differtest，因为流水线中可能发生flush
* 简易调试器无法实时获取cpu寄存器状态，存在延迟
所以需要大改仿真环境，但是我的仿真环境代码(c/c++)已经混乱到一定程度了

所以

上周的TODO我一个都没做，我的时间都用在

- REMU <https://github.com/TimeKeepper/REMU>
- btw，为一个开源项目提交了一个pr <https://github.com/ethanuppal/marlin/pull/110>