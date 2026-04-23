package com.chengwei.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.entity.OperationLog;
import com.chengwei.mapper.OperationLogMapper;
import com.chengwei.service.IOperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {

}
