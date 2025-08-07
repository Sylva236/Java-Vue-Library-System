<template>
  <el-scrollbar height="100%" style="width: 100%">
    <!-- 标题和搜索框 -->
    <div
        style="
        margin-top: 20px;
        margin-left: 40px;
        font-size: 2em;
        font-weight: bold;
      "
    >
      图书信息查询
      <el-button
          type="primary"
          @click="newBookVisible = true"
          style="flex: none;background:#A9AFD1; border-color:#A9AFD1;"
      >
        <el-icon> <Plus /> </el-icon>
        <span> 添加书本 </span></el-button
      >
      <el-button
          type="primary"
          @click="bashNewBookVisible = true"
          style="flex: none;background:#FFA5A5; border-color:#FFA5A5;"
      >批量导入</el-button
      >
    </div>
    <div
        style="
        display: flex;
        flex-wrap: wrap;
        gap: 10px 20px;
        align-items: center;
        justify-content: space-between;
        width: 90%;
        margin: 0 auto;
        padding-top: 2vh;
      "
    >
      <el-input
          v-model="this.toQueryCategory"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入书本类别"
      ></el-input>
      <el-input
          v-model="this.toQueryTitle"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入书名"
      ></el-input>
      <el-input
          v-model="this.toQueryPress"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入出版社"
      ></el-input>
      <el-input
          v-model="this.toQueryMinPublishYear"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入最小出版年份"
      ></el-input>
      <el-input
          v-model="this.toQueryMaxPublishYear"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入最大出版年份"
      ></el-input>
      <el-input
          v-model="this.toQueryAuthor"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入作者"
      ></el-input>
      <el-input
          v-model="this.toQueryMinPrice"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入最小价格"
      ></el-input>
      <el-input
          v-model="this.toQueryMaxPrice"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="输入最大价格"
      ></el-input>
      <el-select
          v-model="this.toQuerySortBy"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="选择排序字段"
      >
        <el-option label="书本编号" value="bookId"></el-option>
        <el-option label="类别" value="category"></el-option>
        <el-option label="标题" value="title"></el-option>
        <el-option label="出版社" value="press"></el-option>
        <el-option label="出版日期" value="publishYear"></el-option>
        <el-option label="作者" value="author"></el-option>
        <el-option label="价格" value="price"></el-option>
        <el-option label="库存" value="stock"></el-option>
      </el-select>
      <el-select
          v-model="this.toQuerySortOrder"
          style="flex: 1; min-width: 150px"
          size="big"
          placeholder="选择排序方式"
      >
        <el-option label="升序" value="ASC"></el-option>
        <el-option label="降序" value="DESC"></el-option>
      </el-select>
      <el-button
          style="flex: 1; min-width: 150px"
          type="primary"
          @click="QueryBooks"
      >查询</el-button
      >
    </div>

    <el-dialog
        v-model="bashNewBookVisible"
        title="批量导入"
        style="min-width: 300px; max-width: 40%;"
        align-center
    >
      <div style="margin: 20px 0; text-align: center">
        <input
            type="file"
            ref="fileInput"
            accept=".txt"
            @change="handleFileSelect"
            style="display: none"
        >
        <el-button
            type="primary"
            @click="$refs.fileInput.click()"
        >
          <el-icon><Upload /></el-icon>
          选择文件
        </el-button>
        <div style="margin-top: 10px; color: #666">
          已选择文件：{{ selectedFile ? selectedFile.name : '未选择' }}
        </div>
      </div>

      <template #footer>
        <span>
          <el-button @click="bashNewBookVisible = false">取消</el-button>
          <el-button
              type="primary"
              @click="handleBatchImport"
              :disabled="!selectedFile"
          >导入</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
        v-model="newBookVisible"
        title="新建书本"
        style="min-width: 300px; max-width: 40%;"
        align-center
    >
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        标题：
        <el-input
            v-model="newBookInfo.title"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        出版社：
        <el-input
            v-model="newBookInfo.press"
            style="width: 12.5vw; margin-left: 24px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        出版年份：
        <el-input
            v-model="newBookInfo.publishYear"
            style="width: 12.5vw; margin-left: 9px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        作者：
        <el-input
            v-model="newBookInfo.author"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        价格：
        <el-input
            v-model="newBookInfo.price"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        类别：
        <el-input
            v-model="newBookInfo.category"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        库存：
        <el-input
            v-model="newBookInfo.stock"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <template #footer>
        <span>
          <el-button @click="newBookVisible = false">取消</el-button>
          <el-button
              type="primary"
              @click="ConfirmNewBook"
              :disabled="!isValidNewBookInfo()"
          >确定</el-button
          >
        </span>
      </template>
    </el-dialog>

    <el-dialog
        v-model="modifyBookVisible"
        :title="'修改信息(书本 ID: ' + this.modifyBookInfo.bookId + ')'"
        style="min-width: 300px; max-width: 40%;"
        align-center
    >
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        标题：
        <el-input
            v-model="modifyBookInfo.title"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        出版社：
        <el-input
            v-model="modifyBookInfo.press"
            style="width: 12.5vw; margin-left: 24px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        出版年份：
        <el-input
            v-model="modifyBookInfo.publishYear"
            style="width: 12.5vw; margin-left: 9px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        作者：
        <el-input
            v-model="modifyBookInfo.author"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        价格：
        <el-input
            v-model="modifyBookInfo.price"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        类别：
        <el-input
            v-model="modifyBookInfo.category"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        库存：
        <el-input
            v-model="modifyBookInfo.stock"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <template #footer>
        <span>
          <el-button @click="modifyBookVisible = false">取消</el-button>
          <el-button
              type="primary"
              @click="ConfirmModifyBook"
              :disabled="!isValidModifyBookInfo()"
          >确定</el-button
          >
        </span>
      </template>
    </el-dialog>

    <el-dialog
        v-model="borrowBookVisible"
        :title="'借阅 ID 为 ' + this.borrowBookInfo.bookId + ' 的书本'"
        style="min-width: 300px; max-width: 40%;"
        align-center
    >
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        借书证ID
        <el-input
            v-model="borrowBookInfo.cardId"
            style="width: 12.5vw; margin-left: 40px"
            clearable
        ></el-input>
      </div>
      <div
          style="
          margin-left: 2vw;
          font-weight: bold;
          font-size: 1rem;
          margin-top: 20px;
        "
      >
        借阅时间
        <el-date-picker
            v-model="borrowBookInfo.borrowTime"
            type="datetime"
            placeholder="选择借阅时间"
            format="YYYY-MM-DD HH:mm:ss"
        />
      </div>
      <template #footer>
        <span>
          <el-button @click="borrowBookVisible = false">取消</el-button>
          <el-button
              type="primary"
              @click="ConfirmBorrowBook"
              :disabled="
              borrowBookInfo.cardId.length === 0 ||
              borrowBookInfo.borrowTime.length === 0
            "
          >确定</el-button
          >
        </span>
      </template>
    </el-dialog>

    <!-- 结果表格 -->
    <el-table
        v-if="isShow"
        :data="tableData"
        height="600"
        :sort-by="toQuerySortBy"
        :sort-orders="toQuerySortOrder === 'ASC' ? ['ascending'] : ['descending']"
        style="table-layout: auto; width: 90%; margin: 30px 5vw 0;"
    >
      <el-table-column prop="bookId" label="书本ID" ></el-table-column>
      <el-table-column prop="category" label="类别" ></el-table-column>
      <el-table-column prop="title" label="书名" ></el-table-column>
      <el-table-column prop="press" label="出版社" ></el-table-column>
      <el-table-column
          prop="publishYear"
          label="出版年份"

      ></el-table-column>
      <el-table-column prop="author" label="作者" ></el-table-column>
      <el-table-column prop="price" label="价格" ></el-table-column>
      <el-table-column prop="stock" label="库存" ></el-table-column>
      <el-table-column
          fixed="right"
          label="操作"
          width="230"
          style="white-space: nowrap"
      >
        <template #default="scope">
          <el-button
              style="width: 50px;background:#6A8D73; border-color:#6A8D73;"
              type="primary"
              @click="
              this.modifyBookInfo.bookId = scope.row.bookId;
              this.modifyBookInfo.category = scope.row.category;
              this.modifyBookInfo.title = scope.row.title;
              this.modifyBookInfo.press = scope.row.press;
              this.modifyBookInfo.publishYear = scope.row.publishYear;
              this.modifyBookInfo.author = scope.row.author;
              this.modifyBookInfo.price = scope.row.price;
              this.modifyBookInfo.stock = scope.row.stock;
              this.modifyBookVisible = true;
            "
          >修改</el-button>
          <el-button
              style="width: 50px;background:#FF4500; border-color:#FF4500;"
              type="primary"
              @click="
              this.toRemove = scope.row.bookId;
              this.removeBookVisible = true;
            "
          >删除</el-button>
          <el-button
              style="width: 50px"
              type="primary"
              @click="
              this.borrowBookInfo.bookId = scope.row.bookId;
              this.borrowBookVisible = true;
            "
              v-if="scope.row.stock > 0"
          >借阅</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="removeBookVisible" title="删除图书"
               style="min-width: 300px; max-width: 40%;" >
      <span
      >确定删除<span style="font-weight: bold">{{ toRemove }}号书本</span
      >吗？</span
      >

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="removeBookVisible = false">取消</el-button>
          <el-button type="danger" @click="ConfirmRemoveBook"> 删除 </el-button>
        </span>
      </template>
    </el-dialog>
  </el-scrollbar>
</template>

<script>
import axios from "axios";
import { ElMessage } from "element-plus";

import * as ElementPlusIconsVue from "@element-plus/icons-vue";

export default {
  data() {
    return {
      isShow: false,
      tableData: [
        {
          bookId: 0,
          category: "",
          title: "",
          press: "",
          publishYear: 0,
          author: "",
          price: 0,
          stock: 0,
        },
      ],
      borrowBookVisible: false,
      borrowBookInfo: {
        bookId: 0,
        cardId: 0,
        borrowTime: new Date().toISOString(),
      },
      toQueryCategory: "",
      toQueryTitle: "",
      toQueryPress: "",
      toQueryMinPublishYear: "",
      toQueryMaxPublishYear: "",
      toQueryAuthor: "",
      toQueryMinPrice: "",
      toQueryMaxPrice: "",
      toQuerySortBy: "",
      toQuerySortOrder: "",
      newBookVisible: false,
      removeBookVisible: false,
      bashNewBookVisible: false,
      toRemove: 0,
      newBookPath: {
        path: "",
        type: "path",
      },
      newBookInfo: {
        category: "",
        title: "",
        press: "",
        publishYear: 0,
        author: "",
        price: 0,
        stock: 0,
      },
      modifyBookVisible: false,
      modifyBookInfo: {
        bookId: 0,
        category: "",
        title: "",
        press: "",
        publishYear: 0,
        author: "",
        price: 0,
        stock: 0,
      },
      selectedFile: null,
    };
  },
  methods: {
    async QueryBooks() {
      this.tableData = [];
      try {
        let response = await axios.get("/books", {
          params: {
            category: this.toQueryCategory,
            title: this.toQueryTitle,
            press: this.toQueryPress,
            minPublishYear: this.toQueryMinPublishYear,
            maxPublishYear: this.toQueryMaxPublishYear,
            author: this.toQueryAuthor,
            minPrice: this.toQueryMinPrice,
            maxPrice: this.toQueryMaxPrice,
            sortBy: this.toQuerySortBy,
            sortOrder: this.toQuerySortOrder,
          },
        });
        if (response.data.success) {
          this.tableData = response.data.data.results;
          this.isShow = true;
        } else {
          ElMessage.error(response.data.error || "获取图书列表失败");
        }
      } catch (error) {
        if (error.response) {
          console.error("服务器错误:", error.response.status, error.response.data);
          ElMessage.error(`服务器错误: ${error.response.status} - ${error.response.data.error || '未知错误'}`);
        } else if (error.request) {
          console.error("未收到响应:", error.request);
          ElMessage.error("服务器未响应，请检查网络连接");
        } else {
          console.error("请求错误:", error.message);
          ElMessage.error("请求错误: " + error.message);
        }
      }
    },
    handleFileSelect(event) {
      this.selectedFile = event.target.files[0];
      event.target.value = ''; // 重置input以便重复选择同一文件
    },
    async handleBatchImport() {
      if (!this.selectedFile) {
        ElMessage.warning("请先选择文件");
        return;
      }

      const reader = new FileReader();
      reader.onload = async (e) => {
        try {
          const content = e.target.result;
          const books = JSON.parse(content);

          // 发送请求
          const response = await axios.post("/books", books);

          if (response.data.success) {
            ElMessage.success(`成功导入${books.length}本图书`);
            this.bashNewBookVisible = false;
            this.QueryBooks();
          } else {
            ElMessage.error(response.data.error || "导入失败");
          }
        } catch (error) {
          let message = "文件处理失败";
          if (error instanceof SyntaxError) {
            message = "文件格式错误，请检查JSON格式";
          } else if (error.response) {
            message = `服务器错误: ${error.response.data.error}`;
          }
          ElMessage.error(message);
        }
      };

      // 读取文件内容
      reader.readAsText(this.selectedFile);
    },
    ConfirmNewBook() {
      axios
          .post("/books", {
            category: this.newBookInfo.category,
            title: this.newBookInfo.title,
            press: this.newBookInfo.press,
            publishYear: this.newBookInfo.publishYear,
            author: this.newBookInfo.author,
            price: this.newBookInfo.price,
            stock: this.newBookInfo.stock,
          })
          .then(response => {
            if (response.data.success) {
              ElMessage.success("新建图书成功");
              this.newBookVisible = false;
              this.QueryBooks();
            } else {
              ElMessage.error(response.data.error || "新建图书失败");
            }
          })
          .catch(error => {
            if (error.response) {
              console.error("服务器错误:", error.response.status, error.response.data);
              ElMessage.error(`服务器错误: ${error.response.status} - ${error.response.data.error || '未知错误'}`);
            } else if (error.request) {
              console.error("未收到响应:", error.request);
              ElMessage.error("服务器未响应，请检查网络连接");
            } else {
              console.error("请求错误:", error.message);
              ElMessage.error("请求错误: " + error.message);
            }
          });
    },
    ConfirmModifyBook() {
      axios
          .put("/books/" + this.modifyBookInfo.bookId, {
            bookId: this.modifyBookInfo.bookId,
            category: this.modifyBookInfo.category,
            title: this.modifyBookInfo.title,
            press: this.modifyBookInfo.press,
            publishYear: this.modifyBookInfo.publishYear,
            author: this.modifyBookInfo.author,
            price: this.modifyBookInfo.price,
            stock: this.modifyBookInfo.stock,
          })
          .then(response => {
            if (response.data.success) {
              ElMessage.success("修改图书成功");
              this.modifyBookVisible = false;
              this.QueryBooks();
            } else {
              ElMessage.error(response.data.error || "修改图书失败");
            }
          })
          .catch(error => {
            if (error.response) {
              console.error("服务器错误:", error.response.status, error.response.data);
              ElMessage.error(`服务器错误: ${error.response.status} - ${error.response.data.error || '未知错误'}`);
            } else if (error.request) {
              console.error("未收到响应:", error.request);
              ElMessage.error("服务器未响应，请检查网络连接");
            } else {
              console.error("请求错误:", error.message);
              ElMessage.error("请求错误: " + error.message);
            }
          });
    },
    ConfirmRemoveBook() {
      axios
          .delete(`/books/${this.toRemove}`)
          .then(response => {
            if (response.data.success) {
              ElMessage.success("删除图书成功");
              this.removeBookVisible = false;
              this.QueryBooks();
            } else {
              ElMessage.error(response.data.error || "删除图书失败");
            }
          })
          .catch(error => {
            if (error.response) {
              console.error("服务器错误:", error.response.status, error.response.data);
              ElMessage.error(`服务器错误: ${error.response.status} - ${error.response.data.error || '未知错误'}`);
            } else if (error.request) {
              console.error("未收到响应:", error.request);
              ElMessage.error("服务器未响应，请检查网络连接");
            } else {
              console.error("请求错误:", error.message);
              ElMessage.error("请求错误: " + error.message);
            }
          });
    },
    ConfirmBorrowBook() {
      const borrowTime = new Date(this.borrowBookInfo.borrowTime).getTime();
      axios
          .post("/borrows", {
            bookId: this.borrowBookInfo.bookId,
            cardId: this.borrowBookInfo.cardId,
            borrowTime: borrowTime
          })
          .then(response => {
            if (response.data.success) {
              ElMessage.success("借阅图书成功");
              this.borrowBookVisible = false;
              this.QueryBooks();
            } else {
              ElMessage.error(response.data.error || "借阅图书失败");
            }
          })
          .catch(error => {
            if (error.response) {
              console.error("服务器错误:", error.response.status, error.response.data);
              ElMessage.error(`服务器错误: ${error.response.status} - ${error.response.data.error || '未知错误'}`);
            } else if (error.request) {
              console.error("未收到响应:", error.request);
              ElMessage.error("服务器未响应，请检查网络连接");
            } else {
              console.error("请求错误:", error.message);
              ElMessage.error("请求错误: " + error.message);
            }
          });
    },
    isValidNewBookInfo() {
      return (
          this.newBookInfo.title != "" &&
          this.newBookInfo.price > 0 &&
          this.newBookInfo.stock > 0 &&
          this.newBookInfo.author != "" &&
          this.newBookInfo.press != "" &&
          this.newBookInfo.publishYear > 0
      );
    },
    isValidPath() {
      return this.newBookPath != "";
    },
    isValidModifyBookInfo() {
      return (
          this.modifyBookInfo.title != "" &&
          this.modifyBookInfo.price > 0 &&
          this.modifyBookInfo.stock >= 0 &&
          this.modifyBookInfo.author != "" &&
          this.modifyBookInfo.press != "" &&
          this.modifyBookInfo.publishYear > 0
      );
    },
  },
  mounted() {
    this.QueryBooks();
  },
};
</script>