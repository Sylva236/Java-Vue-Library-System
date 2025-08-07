<template>
    <el-scrollbar height="100%" style="width: 100%;">

        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">
            借书记录查询
        </div>

        <!-- 查询框 -->
        <div style="width:30%;margin:0 auto; padding-top:5vh;">

            <el-input v-model="this.toQuery" style="display:inline; " placeholder="输入借书证ID"></el-input>
            <el-button style="margin-left: 10px;" type="primary" @click="QueryBorrows">查询</el-button>

        </div>

        <!-- 结果表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600"
            :default-sort="{ prop: 'borrowTime', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="cardId" label="借书证ID" />
            <el-table-column prop="bookId" label="图书ID" sortable />
          <!-- 调整el-table-column的渲染方式 -->
          <el-table-column prop="borrowTime" label="借出时间" sortable>
            <template #default="{ row }">
              {{ formatDate(row.borrowTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="returnTime" label="归还时间" sortable>
            <template #default="{ row }">
              {{ row.returnTime !== 0 ? formatDate(row.returnTime) : '未归还' }}
            </template>
          </el-table-column>
            <el-table-column label="归还操作">
              <template #default="scope">
                <el-button
                    style="margin-left:2%;width:80px"
                    type="primary"
                    @click="openReturnDialog(scope.row)"
                    v-if="scope.row.returnTime === 0"
                >归还</el-button>
              </template>
            </el-table-column>
        </el-table>

      <!-- 还书对话框 -->
      <el-dialog
          v-model="returnBookVisible"
          :title="'归还 BookID: ' + returnBookInfo.bookId"
          width="30%"
          align-center
      >
        <div style="margin:20px 0">
          <span style="margin-right:10px">借出时间：</span>
          {{ formatDate(returnBookInfo.borrowTime) }}
        </div>
        <div>
          <span style="margin-right:10px">归还时间：</span>
          <el-date-picker
              v-model="returnBookInfo.returnTimeH"
              type="datetime"
              placeholder="选择归还时间"
              value-format="YYYY-MM-DDTHH:mm:ss.SSSZ"
              @change="convertToTimestamp"
          />
        </div>
        <template #footer>
          <el-button @click="returnBookVisible = false">取消</el-button>
          <el-button
              type="primary"
              @click="ConfirmReturnBook"
              :disabled="!isReturnTimeValid"
          >确定</el-button>
        </template>
      </el-dialog>

    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
    data() {
        return {
            isShow: false, // 结果表格展示状态
            tableData: [],
            toQuery: '', // 待查询内容
            toSearch: '', // 待搜索内容
            Search,
            returnBookVisible: false,
            returnBookInfo: {
              bookId: 0,
              cardId: 0,
              borrowTime: 0,
              returnTimeH: new Date().toISOString(),
              returnTime: 0
            },
        }
    },
    computed: {
        fitlerTableData() { // 搜索规则
            return this.tableData.filter(
                (tuple) =>
                    (this.toSearch == '') || // 搜索框为空，即不搜索
                    tuple.bookId == this.toSearch || // 图书号与搜索要求一致
                    tuple.borrowTime.toString().includes(this.toSearch) || // 借出时间包含搜索要求
                    tuple.returnTime.toString().includes(this.toSearch) // 归还时间包含搜索要求
            )
        },
        isReturnTimeValid() {
          return this.returnBookInfo.returnTime > this.returnBookInfo.borrowTime;
        }
    },
    methods: {
        async QueryBorrows() {
          try {
            this.tableData = [];
            console.log('Fetching data for card:', this.toQuery);
            //const response = await axios.get(`/borrows/${this.toQuery}`);
            const response = await axios.get(`/borrows/${this.toQuery}?t=${Date.now()}`); // 防止缓存
            console.log('Response data:', response.data); // 检查返回数据
            if (response.data.success) {
              this.tableData = response.data.data.items.map(item => ({
                ...item,
                borrowTime: item.borrowTime,
                returnTime: item.returnTime
              }));
              console.log('Updated tableData:', this.tableData); // 检查数据是否正确
              this.isShow = true;
            } else {
              ElMessage.error(response.data.message || "获取借阅记录失败");
            }
          } catch (error) {
            if (error.response) {
              ElMessage.error(`服务器错误: ${error.response.status}`);
            } else {
              ElMessage.error("网络请求失败，请检查连接");
            }
          }
        },
        // 打开还书对话框
        openReturnDialog(row) {
          this.returnBookInfo = {
            bookId: row.bookId,
            cardId: row.cardId,
            borrowTime: row.borrowTime,
            returnTimeH: new Date().toISOString(), // 默认当前时间
            returnTime: 0
          };
          this.returnBookVisible = true;
        },

        // 将选择的日期时间转换为时间戳
        convertToTimestamp(val) {
          this.returnBookInfo.returnTime = new Date(val).getTime();
        },

        //时间格式化工具
        formatDate(timestamp) {
          return new Date(timestamp).toLocaleString();
        },

        // 提交还书请求
        async ConfirmReturnBook() {
          try {
            const response = await axios.put(`/borrows`, {
              bookId: this.returnBookInfo.bookId,
              cardId: this.returnBookInfo.cardId,
              borrowTime: this.returnBookInfo.borrowTime,
              returnTime: this.returnBookInfo.returnTime
            }).catch(error => { // 显式捕获 axios 错误
              console.error('请求失败:', error);
              throw error; // 继续抛出到外层 catch
            });
            if (response.data.success) {
              ElMessage.success("还书成功");
              //console.log('Closing dialog');
              this.returnBookVisible = false;
              await this.QueryBorrows(); // 刷新列表
            } else {
              console.error('业务逻辑错误:', response.data);
              ElMessage.error(response.data.error || "还书失败");
            }
          } catch (error) {
            if (error.response) {
              ElMessage.error(`服务器错误: ${error.response.status}`);
            } else {
              ElMessage.error("网络请求失败");
            }
          }
        }

    }
}
</script>