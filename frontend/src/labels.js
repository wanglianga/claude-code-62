export const ROLES = {
  FAMILY: '家属',
  TRANSPORT: '接运组',
  CLERK: '业务员',
  FINANCE: '财务',
  HALL_ADMIN: '礼厅管理员',
  CREMATORIUM: '火化组',
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
  COLD: '冷藏存放',
  CREMATION: '火化',
  EMBALM: '化妆整容',
  FAREWELL: '告别仪式/礼厅',
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
  CREMATION_CONFLICT: ['火化排期冲突', 'CREMATORIUM'],
  COLD_SHORTAGE: ['冷藏位不足', 'HALL_ADMIN'],
  VEHICLE_DELAY: ['车辆延误', 'TRANSPORT'],
  VEHICLE_ISSUE: ['车辆资质异常', 'TRANSPORT'],
  PERMIT_MISSING: ['接运许可待补', 'TRANSPORT'],
  NONLOCAL: ['外地逝者协同', 'TRANSPORT'],
  GENERAL: ['其他协同', 'CLERK']
}

export const CROSS_STATUS = {
  PLANNED: ['已登记待核验', 'gray'],
  IN_TRANSIT: ['接运在途', 'blue'],
  ARRIVED: ['已到馆交接', 'green'],
  SUSPENDED: ['异常暂停', 'red']
}

export const PERMIT_STATUS = {
  PENDING: ['待核验', 'gray'], VERIFIED: ['已核验', 'green'], MISSING: ['缺失', 'red']
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
  NONE: ['未申请', 'gray'],
  PENDING: ['待财务初审', 'amber'],
  FINANCE_PRE_APPROVED: ['财务初审通过·待馆领导确认', 'blue'],
  APPROVED: ['终审通过', 'green'],
  REJECTED: ['审核未通过', 'red']
}

export const ASSISTANCE_TYPE = {
  SUBSISTENCE: '低保救助',
  EXTREME_POVERTY: '特困人员救助',
  TEMP_RELIEF: '临时救助'
}

export const REDUCTION_APP_STATUS = {
  SUBMITTED: ['待财务初审', 'amber'],
  FINANCE_PRE_APPROVED: ['待馆领导确认', 'blue'],
  APPROVED: ['终审通过', 'green'],
  REJECTED: ['未通过', 'red']
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
