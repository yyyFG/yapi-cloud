// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /user/image/upload */
/** 后端接口为 @RequestPart MultipartFile file，需以 multipart/form-data 提交，文件字段名为 file */
export async function imageUpload(file: File, options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('file', file)
  return request<API.BaseResponseString>('/user/image/upload', {
    method: 'POST',
    // 不手动设置 Content-Type：axios 检测到 FormData 会自动带上 multipart/form-data 及 boundary
    data: formData,
    ...(options || {}),
  })
}
