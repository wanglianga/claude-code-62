export const ROLES = {
  FAMILY: '家属',
  TRANSPORT: '接运组',
  CLERK: '业务员',
  FINANCE: '财务',
  HALL_ADMIN: '礼厅管理员',
  LEADER: '馆领导'
}

export const ORDER_STATUS = {
  DRAFT: ['待提交', 'gray'],
  RESOURCE_VERIFYING: ['资源核验中', 'amber'],
  VERIFIED: ['核验通过', 'blue'],
  NEGOTIATING: ['方案沟通中', 'amber'],
  CONFIRMED: ['方案已确认', 'purple'],
  IN_SERVICE: ['服务进行中', 'blue'],
  COMPLETED: ['服务完成', 'blue'],
  SETTLED: ['已结算', 'green'],
  ARCHIVED: ['已归档', 'gray'],
  CANCELLED: ['已取消', 'red']
}

export const SERVICE_CLASS = {
  PUBLIC_BASIC: ['公益基本服务', 'blue'],
  OPTIONAL: ['自选增值服务', 'amber'],
  SUBSIDY: ['政府补助项目', 'green']
}

export const ITEM_STATUS = {
  PENDING: ['待家属确认', 'amber'],
  CONFIRMED: ['已确认', 'green'],
  REMOVED: ['已删减', 'red']
}

export const ITEM_CATEGORY = {
  TRANSPORT: '遗体接运',
  EMBALM: '化妆整容',
  FAREWELL: '告别仪式',
  WREATH: '花圈挽联',
  BURIAL_CLOTHES: '寿衣',
  URN: '骨灰盒',
  CATERING: '餐饮',
  REST_ROOM: '休息室',
  OTHER: '其他'
}

export const HALL_SPEC = {
  SMALL: '小型厅', MEDIUM: '中型厅', LARGE: '大型厅', GRAND: '特级厅'
}

export const RES_TYPE = {
  VEHICLE: '接运车辆', COLD: '冷藏位', HALL: '告别厅', FURNACE: '火化设备'
}

export const COMM_TYPE = {
  CHAT: '普通沟通',
  DISPUTE: '亲属意见不一致',
  EMOTIONAL: '家属情绪激动',
  TEMP_CHANGE: '现场临时变更',
  OTHER: '其他'
}

export const COLLAB_TYPE = {
  DOC_MISSING: ['证明材料缺失', 'CLERK'],
  FAMILY_DISAGREE: ['亲属意见不一致', 'LEADER'],
  REDUCTION_REVIEW: ['低保减免待审核', 'FINANCE'],
  HALL_CONFLICT: ['告别厅临时冲突', 'HALL_ADMIN'],
  FURNACE_MAINT: ['火化设备检修', 'HALL_ADMIN'],
  NONLOCAL: ['外地逝者协同', 'TRANSPORT'],
  GENERAL: ['其他协同', 'CLERK']
}

export const CERT_STATUS = {
  PENDING: ['待核验', 'gray'],
  VERIFIED: ['已核验', 'green'],
  MISSING: ['材料缺失', 'red']
}

export const PAY_STATUS = {
  UNPAID: ['未收款', 'red'], PARTIAL: ['部分收款', 'amber'], PAID: ['已结清', 'green']
}

export const REDUCTION_STATUS = {
  NONE: ['未申请', 'gray'], PENDING: ['待审核', 'amber'], APPROVED: ['已批准', 'green'], REJECTED: ['未通过', 'red']
}

export const COLLAB_STATUS = { OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }

export function tag(map, key) {
  const v = map[key]
  return v ? { text: v[0], cls: v[1] } : { text: key || '-', cls: 'gray' }
}
export function txt(map, key) { return (map[key] && map[key][0]) || map[key] || key || '-' }

export function fmtTime(s) {
  if (!s) return '-'
  return String(s).replace('T', ' ').substring(0, 16)
}
export function fmtMoney(n) {
  const v = Number(n || 0)
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
